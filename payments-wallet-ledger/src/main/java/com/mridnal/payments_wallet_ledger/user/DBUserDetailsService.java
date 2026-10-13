package com.mridnal.payments_wallet_ledger.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DBUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String identity) {
        User user = userRepository.findByEmailOrPhone(identity.trim())
                .orElseThrow(() ->
                        new UsernameNotFoundException("Invalid credentials"));

        if (user.getPasswordHash() == null
                || user.getPasswordHash().isBlank()) {
            throw new UsernameNotFoundException("Invalid credentials");
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(identity.trim())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .build();
    }
}

