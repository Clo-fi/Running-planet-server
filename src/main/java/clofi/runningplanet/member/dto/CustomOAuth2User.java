package clofi.runningplanet.member.dto;

import java.util.Collection;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import clofi.runningplanet.member.domain.Member;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CustomOAuth2User implements OAuth2User {

	private final Member member;

	@Override
	public Map<String, Object> getAttributes() {
		return null;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return null;
	}

	@Override
	public String getName() {
		return member.getNickname();
	}
	
	public Long getId() {
		return member.getId();
	}

}
