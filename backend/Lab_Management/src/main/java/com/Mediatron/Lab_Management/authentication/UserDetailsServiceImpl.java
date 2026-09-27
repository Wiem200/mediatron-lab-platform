package com.Mediatron.Lab_Management.authentication;

import com.Mediatron.Lab_Management.data_models.User;
import com.Mediatron.Lab_Management.repositories.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Use Spring's Transactional

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    // Constructor Injection (No need for @Autowired on modern Spring, but okay to keep)
    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional // Ensures the database session stays open to load the Roles
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        // 1. Fetch user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with email: " + email));

        // 2. Return the Principal
        // If you used my static "build" method from before, use: UserPrincipal.build(user);
        // If you kept your simple constructor, use:
        return  UserPrincipal.build(user);
    }
}