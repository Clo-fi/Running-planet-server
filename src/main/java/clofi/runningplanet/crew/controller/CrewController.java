package clofi.runningplanet.crew.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import clofi.runningplanet.crew.dto.request.ApplyCrewReqDto;
import clofi.runningplanet.crew.dto.request.CreateCrewReqDto;
import clofi.runningplanet.crew.dto.request.ProceedApplyReqDto;
import clofi.runningplanet.crew.dto.response.ApplyCrewResDto;
import clofi.runningplanet.crew.dto.response.ApprovalMemberResDto;
import clofi.runningplanet.crew.dto.response.FindAllCrewResDto;
import clofi.runningplanet.crew.dto.response.FindCrewResDto;
import clofi.runningplanet.crew.service.CrewService;
import clofi.runningplanet.member.dto.CustomOAuth2User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
public class CrewController {

	private final CrewService crewService;

	@PostMapping("/api/crew")
	public ResponseEntity<Void> createCrew(@RequestBody @Valid CreateCrewReqDto reqDto,
		@AuthenticationPrincipal CustomOAuth2User principal) {
		Long crewId = crewService.createCrew(reqDto, principal.getId());
		return ResponseEntity.created(URI.create("/api/crew/" + crewId)).build();
	}

	@GetMapping("/api/crew")
	public ResponseEntity<List<FindAllCrewResDto>> findAllCrews() {
		return ResponseEntity.ok(crewService.findAllCrew());
	}

	@GetMapping("/api/crew/{crewId}")
	public ResponseEntity<FindCrewResDto> findCrew(@PathVariable("crewId") Long crewId) {
		return ResponseEntity.ok(crewService.findCrew(crewId));
	}

	@PostMapping("/api/crew/{crewId}")
	public ResponseEntity<ApplyCrewResDto> applyCrew(@PathVariable("crewId") Long crewId,
		@RequestBody ApplyCrewReqDto reqDto,
		@AuthenticationPrincipal CustomOAuth2User principal) {
		return ResponseEntity.status(HttpStatus.CREATED).body(crewService.applyCrew(reqDto, crewId, principal.getId()));
	}

	@GetMapping("/api/crew/{crewId}/request")
	public ResponseEntity<ApprovalMemberResDto> getApplyCrewList(@PathVariable("crewId") Long crewId,
		@AuthenticationPrincipal CustomOAuth2User principal) {
		return ResponseEntity.ok(crewService.getApplyCrewList(crewId, principal.getId()));
	}

	@PostMapping("/api/crew/{crewId}/request")
	public ResponseEntity<Void> proceedApplyCrew(@PathVariable("crewId") Long crewId,
		@RequestBody ProceedApplyReqDto reqDto, @AuthenticationPrincipal CustomOAuth2User principal) {
		crewService.proceedApplyCrew(reqDto, crewId, principal.getId());
		return ResponseEntity.ok().build();
	}
}
