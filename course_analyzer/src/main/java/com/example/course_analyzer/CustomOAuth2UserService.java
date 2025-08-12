package com.example.course_analyzer;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId(); // "kakao"
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName(); // "id"

        Map<String, Object> attributes = oAuth2User.getAttributes();

        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        String provider = registrationId;
        String providerId = attributes.get(userNameAttributeName).toString();
        String nickname = (String) profile.get("nickname");
        String email = (String) kakaoAccount.get("email");
        String username = provider + "_" + providerId; // e.g., kakao_1234567890

        User user = saveOrUpdate(username, nickname, email, provider, providerId);

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                userNameAttributeName);
    }

    private User saveOrUpdate(String username, String nickname, String email, String provider, String providerId) {
        Optional<User> optionalUser = userRepository.findByProviderAndProviderId(provider, providerId);
        User user;

        if (optionalUser.isPresent()) {
            user = optionalUser.get();
            user.update(nickname); // Call the update method directly
        } else {
            user = User.builder()
                    .username(username)
                    .nickname(nickname)
                    .email(email)
                    .provider(provider)
                    .providerId(providerId)
                    .build();
        }

        return userRepository.save(user);
    }
}
