package com.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.demo.dao.UserRepository;
import com.demo.entities.User;
import com.demo.helper.Message;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class HomeController {

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;
	
	
	@Autowired
	private UserRepository userRepository;

	@RequestMapping("/")
	public String home(Model model) {
		model.addAttribute("title", "Home - smart contact manager");
		return "home";
	}

	@RequestMapping("/about")
	public String about(Model model) {
		model.addAttribute("title", "About - smart contact manager");
		return "about";
	}

	@RequestMapping("/signup")
	public String signup(Model model) {
		model.addAttribute("title", "signup - smart contact manager");
		model.addAttribute("user", new User());
		return "signup";
	}

	@RequestMapping(value = "/do_register", method = RequestMethod.POST)
	public String handleRegister(@Valid @ModelAttribute("user") User user, BindingResult result1,
			@RequestParam(value = "agreement", defaultValue = "false") boolean agreement, Model model,
			HttpSession session) {

		try {
			if (!agreement) {
				System.out.println("please agree the terms and condition");
				throw new Exception("please agree the terms and condition");
			}

			if (result1.hasErrors()) {
				model.addAttribute("user", user);
				return "signup";
			}

			user.setRole("ROLE_USER");
			user.setEnabled(true);
			user.setImageUrl("default.png");
			user.setPassword(passwordEncoder.encode(user.getPassword()));

			User res = this.userRepository.save(user);

			model.addAttribute("user", new User());
			session.setAttribute("message1", new Message("Successfully Registered!", "alert-success"));

		} catch (Exception e) {
			e.printStackTrace();
			model.addAttribute("user", user);
			session.setAttribute("message1", new Message("Something went wrong!", "alert-danger"));
		}
		return "signup"; // Return the name of the success page
	}

	@GetMapping("/login")
	public String customLogin(Model model) {
		return "login";
	}
	
	@GetMapping("/login-fail")
    public String loginFail() {
        return "login-fail"; // Make sure this maps to login-fail.html
    }
	
}
