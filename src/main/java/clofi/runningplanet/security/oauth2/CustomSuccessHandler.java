package clofi.runningplanet.security.oauth2;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import clofi.runningplanet.member.dto.CustomOAuth2User;
import clofi.runningplanet.security.jwt.JWTUtil;
import clofi.runningplanet.security.jwt.JwtToken;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	private final JWTUtil jwtUtil;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws
		IOException {

		//OAuth2User
		CustomOAuth2User customUserDetails = (CustomOAuth2User) authentication.getPrincipal();

		String username = customUserDetails.getName();

		JwtToken jwt = jwtUtil.createJwt(username, 60 * 60 * 60L * 1000);

		response.addCookie(createCookie("Authorization", jwt.getAccessToken()));
		response.sendRedirect("http://localhost:5173");

	}

	private Cookie createCookie(String key, String value) {

		Cookie cookie = new Cookie(key, value);
		cookie.setMaxAge(60*60*60);
		cookie.setSecure(true);
		cookie.setAttribute("SameSite","None");
		cookie.setPath("/");
		cookie.setHttpOnly(true);

		return cookie;

	}

}
