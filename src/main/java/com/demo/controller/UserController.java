package com.demo.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.demo.dao.ContactRepository;
import com.demo.dao.UserRepository;
import com.demo.entities.Contact;
import com.demo.entities.User;
import com.demo.helper.Message;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ContactRepository contactRepository;

	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		String username = principal.getName();
		User user = userRepository.getUserByUserName(username);

		model.addAttribute("user", user);
	}

	@RequestMapping("/index")
	public String dashboard(Model model, Principal principal) {
		model.addAttribute("title", "User Dashboard");

		return "normal/user_dashboard";
	}

	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {
		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());

		return "normal/add-contact-form";
	}

	// handler for add contact form
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact, @RequestParam("imageUpload") MultipartFile file,
			Principal principal, BindingResult bresult, HttpSession session) {

		try {
			if (bresult.hasErrors()) {
				session.setAttribute("message", new Message("Something went wrong", "alert-danger"));
				return "normal/add-contact-form";
			}

			if (file.isEmpty()) {
				// set a default profile image
				contact.setImage("default1.png");

			} else {
				contact.setImage(file.getOriginalFilename());
				File saveFile = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			}

			String name = principal.getName();
			User user = this.userRepository.getUserByUserName(name);
			contact.setUser(user);

			user.getContacts().add(contact);
			this.userRepository.save(user);
			session.setAttribute("message", new Message("Successfully added the contact", "alert-success"));

		} catch (Exception e) {
			e.printStackTrace();
			session.setAttribute("message", new Message("Something went wrong", "alert-danger"));
		}

		return "normal/add-contact-form";
	}

	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page, Model model, Principal principal) {

		model.addAttribute("title", "show contacts");

		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);

		Pageable pageable = PageRequest.of(page, 3);

		Page<Contact> contacts = this.contactRepository.findContactsByUser(user.getId(), pageable);

		model.addAttribute("contacts", contacts);
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", contacts.getTotalPages());

		return "normal/show_contacts";
	}

	// show a particular contact details
	@RequestMapping("/{cId}/contact")
	public String showContactDetails(@PathVariable("cId") Integer cId, Model model, Principal principal) {

		Contact contact = this.contactRepository.findById(cId).get();

		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);

		if (user.getId() == contact.getUser().getId()) {
			model.addAttribute("contact", contact);
		}

		return "normal/contact_detail";
	}

	// delete contact handler
	@GetMapping("/delete/{cId}")
	public String deleteContact(@PathVariable("cId") Integer cId, Model model, Principal principal,
			HttpSession session) {

		try {
			String userName = principal.getName();
			User user = this.userRepository.getUserByUserName(userName);

			Contact contact = this.contactRepository.findById(cId).get();

			if (user.getId() == contact.getUser().getId()) {
			//contact.setUser(null);
			//this.contactRepository.delete(contact);
				user.getContacts().remove(contact);
				this.userRepository.save(user);

				String imageName = contact.getImage();
				File saveFile = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + imageName);
                Files.deleteIfExists(path);

				session.setAttribute("message", new Message("successfully deleted", "alert-success"));
			} else {
				session.setAttribute("message", new Message("something went wrong", "alert-danger"));

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "redirect:/user/show-contacts/0";
	}
	
	
	// open update form handler 
	@PostMapping("/update-contact/{cid}")
	public String updateForm(@PathVariable("cid") Integer cId, Model model) {
		model.addAttribute("title", "update form");
		
		Contact contact = this.contactRepository.findById(cId).get();
		
		model.addAttribute("contact", contact);
		return "normal/update_form";
	}
	
	// update contact handler
	@PostMapping("/process-update")
	public String updateHandler(@ModelAttribute Contact contact, 
			@RequestParam("imageUpload") MultipartFile file,
			Model model,
			HttpSession session, 
			Principal principal) {
		try {
			Contact oldContactDetail = this.contactRepository.findById(contact.getcId()).get();
			
			if(!file.isEmpty()) {
				// upload new file - rewrite
				// delete old photo
				File deleteFile = new ClassPathResource("static/img").getFile();
				File file1 = new File(deleteFile, oldContactDetail.getImage());
				file1.delete();
				
				// update new photo
				File saveFile = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			    contact.setImage(file.getOriginalFilename());
				
				
			} else {
				contact.setImage(oldContactDetail.getImage());
			}
			
			User user = this.userRepository.getUserByUserName(principal.getName());
			contact.setUser(user);
			this.contactRepository.save(contact);
			
			session.setAttribute("message", new Message("Successfully updated your contact", "alert-success"));
			
			
		} catch (Exception e) {
				e.printStackTrace();			
		}
		return "redirect:/user/"+contact.getcId() + "/contact";
	}
	
	// your profile handler
	@GetMapping("/profile")
	public String yourProfile(Model model, Principal principal) {
		model.addAttribute("title", "profile page");
		User user = this.userRepository.getUserByUserName(principal.getName());
		model.addAttribute("user", user);
		
		return "normal/profile";
	}
	

}
