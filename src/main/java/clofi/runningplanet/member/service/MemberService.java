package clofi.runningplanet.member.service;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import clofi.runningplanet.member.domain.Member;
import clofi.runningplanet.member.domain.OAuthType;
import clofi.runningplanet.member.domain.SocialLogin;
import clofi.runningplanet.member.dto.CustomOAuth2User;
import clofi.runningplanet.member.dto.KakaoResponse;
import clofi.runningplanet.member.dto.OAuth2Response;
import clofi.runningplanet.member.repository.MemberRepository;
import clofi.runningplanet.member.repository.SocialLoginRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MemberService extends DefaultOAuth2UserService {

	private final MemberRepository memberRepository;
	private final SocialLoginRepository socialLoginRepository;

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

		OAuth2User oAuth2User = super.loadUser(userRequest);

		String registrationId = userRequest.getClientRegistration().getRegistrationId();

		OAuth2Response oAuth2Response = null;
		if (registrationId.equals("kakao")) {
			oAuth2Response = new KakaoResponse(oAuth2User.getAttributes());

			if (oAuth2Response.getName() == null || oAuth2Response.getProfileImage() == null) {
				throw new OAuth2AuthenticationException(new OAuth2Error(
					"invalid_kakao_response", "Invalid Kakao response data", null));
			}

			String oAuthType = oAuth2Response.getProvider();
			String oAuthId = oAuth2Response.getProviderId();

			if (!socialLoginRepository.existsByOauthTypeAndOauthId(OAuthType.valueOf(oAuthType.toUpperCase()),
				oAuthId)) {

				Member member = Member.builder()
					.nickname(oAuth2Response.getName())
					.profileImg(oAuth2Response.getProfileImage())
					.build();
				Member savedMember = memberRepository.save(member);

				SocialLogin socialLogin = SocialLogin.builder()
					.member(savedMember)
					.oauthId(oAuth2Response.getProviderId())
					.oauthType(OAuthType.valueOf(oAuth2Response.getProvider().toUpperCase()))
					.externalEmail(oAuth2Response.getEmail())
					.build();
				socialLoginRepository.save(socialLogin);

				return new CustomOAuth2User(savedMember);

			} else {

				SocialLogin socialLogin = socialLoginRepository.findByOauthTypeAndOauthId(
					OAuthType.valueOf(oAuthType.toUpperCase()), oAuthId);
				Member member = socialLogin.getMember();

				//TODO 패치조인으로 수정
				log.info(member.getNickname());

				return new CustomOAuth2User(member);
			}

		} else {
			//로그인 id가 일치하지 않을 경우
			OAuth2Error oauth2Error = new OAuth2Error("invalid_registration_id",
				"The registration id is invalid: " + registrationId, null);
			throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.getDescription());
		}
	}

}
