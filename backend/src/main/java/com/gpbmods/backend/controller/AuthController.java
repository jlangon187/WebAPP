package com.gpbmods.backend.controller;

import com.gpbmods.backend.dto.AuthResponse;
import com.gpbmods.backend.dto.LoginRequest;
import com.gpbmods.backend.dto.ProfileUpdateRequest;
import com.gpbmods.backend.dto.RegisterRequest;
import com.gpbmods.backend.model.Usuario;
import com.gpbmods.backend.repository.UsuarioRepository;
import com.gpbmods.backend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.beans.factory.annotation.Value;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${discord.client.id}")
    private String discordClientId;

    @Value("${discord.client.secret}")
    private String discordClientSecret;

    @Value("${discord.redirect.uri}")
    private String discordRedirectUri;

    @Value("${frontend.url}")
    private String frontendUrl;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest signUpRequest) {
        if (usuarioRepository.findByEmail(signUpRequest.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Error: Email is already in use!");
        }

        String guid = signUpRequest.getGuid();
        if (guid == null || guid.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Error: El GUID es obligatorio.");
        }

        guid = guid.trim().toUpperCase();
        if (!guid.matches("^[A-F0-9]{8}$")) {
            return ResponseEntity.badRequest()
                    .body("Error: El formato del GUID debe ser de 8 caracteres hexadecimales (ej. 51C617A2).");
        }

        if (usuarioRepository.findByGuid(guid).isPresent()) {
            return ResponseEntity.badRequest().body("Error: Este GUID ya está registrado.");
        }

        Usuario user = new Usuario();
        user.setNombre(signUpRequest.getNombre());
        user.setEmail(signUpRequest.getEmail());
        user.setPasswordHash(passwordEncoder.encode(signUpRequest.getPassword()));
        user.setGuid(guid);
        user.setRol(Usuario.Rol.registrado);

        usuarioRepository.save(user);

        return ResponseEntity.ok("User registered successfully!");
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        Optional<Usuario> userOpt = usuarioRepository.findByEmail(loginRequest.getEmail());

        if (userOpt.isPresent()) {
            Usuario user = userOpt.get();
            if (passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
                String jwt = jwtUtil.generateToken(user.getEmail(), user.getRol().name());
                return ResponseEntity.ok(new AuthResponse(jwt, user.getRol().name(), user.getGuid(), user.getNombre()));
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: Invalid credentials");
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody ProfileUpdateRequest request, @RequestHeader("Authorization") String tokenHeader) {
        try {
            String token = tokenHeader.substring(7);
            String email = jwtUtil.getEmailFromToken(token);
            Optional<Usuario> userOpt = usuarioRepository.findByEmail(email);
            
            if (userOpt.isPresent()) {
                Usuario user = userOpt.get();
                boolean emailChanged = false;

                if (request.getNombre() != null && !request.getNombre().trim().isEmpty()) {
                    user.setNombre(request.getNombre().trim());
                }
                if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
                    user.setPasswordHash(passwordEncoder.encode(request.getPassword().trim()));
                }
                if (request.getGuid() != null && !request.getGuid().trim().isEmpty()) {
                    String guid = request.getGuid().trim().toUpperCase();
                    if (!guid.matches("^[A-F0-9]{8}$")) {
                        return ResponseEntity.badRequest().body("Error: El GUID debe ser de 8 caracteres hexadecimales.");
                    }
                    if (!guid.equals(user.getGuid()) && usuarioRepository.findByGuid(guid).isPresent()) {
                        return ResponseEntity.badRequest().body("Error: Este GUID ya está registrado.");
                    }
                    user.setGuid(guid);
                }
                if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
                    String newEmail = request.getEmail().trim();
                    if (!newEmail.equals(user.getEmail()) && usuarioRepository.findByEmail(newEmail).isPresent()) {
                        return ResponseEntity.badRequest().body("Error: Este email ya está registrado.");
                    }
                    if (!newEmail.equals(user.getEmail())) {
                        user.setEmail(newEmail);
                        emailChanged = true;
                    }
                }
                
                usuarioRepository.save(user);

                if (emailChanged) {
                    // Generate new token because the subject (email) changed
                    String newJwt = jwtUtil.generateToken(user.getEmail(), user.getRol().name());
                    return ResponseEntity.ok(new AuthResponse(newJwt, user.getRol().name(), user.getGuid(), user.getNombre()));
                }

                return ResponseEntity.ok(new AuthResponse(token, user.getRol().name(), user.getGuid(), user.getNombre()));
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }
    }

    @GetMapping("/discord/login")
    public void discordLogin(HttpServletResponse response) throws IOException {
        String url = "https://discord.com/api/oauth2/authorize?client_id=" + discordClientId +
                "&redirect_uri=" + URLEncoder.encode(discordRedirectUri, StandardCharsets.UTF_8) +
                "&response_type=code&scope=identify%20email";
        response.sendRedirect(url);
    }

    @GetMapping("/discord/callback")
    public void discordCallback(@RequestParam("code") String code, HttpServletResponse response) throws IOException {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", discordClientId);
            body.add("client_secret", discordClientSecret);
            body.add("grant_type", "authorization_code");
            body.add("code", code);
            body.add("redirect_uri", discordRedirectUri);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> tokenResponse = restTemplate.postForEntity("https://discord.com/api/oauth2/token",
                    request, Map.class);
            String accessToken = (String) tokenResponse.getBody().get("access_token");

            HttpHeaders userHeaders = new HttpHeaders();
            userHeaders.setBearerAuth(accessToken);
            HttpEntity<?> userRequest = new HttpEntity<>(userHeaders);
            ResponseEntity<Map> userResp = restTemplate.exchange("https://discord.com/api/users/@me", HttpMethod.GET,
                    userRequest, Map.class);

            String discordId = (String) userResp.getBody().get("id");
            String username = (String) userResp.getBody().get("username");
            String email = (String) userResp.getBody().get("email");
            if (email == null)
                email = discordId + "@discord.com";

            Optional<Usuario> userOpt = usuarioRepository.findByEmail(email);
            Usuario user;
            if (userOpt.isPresent()) {
                user = userOpt.get();
            } else {
                user = new Usuario();
                user.setNombre(username);
                user.setEmail(email);
                user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
                // Generate random 8-char hex string
                String randomGuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                user.setGuid(randomGuid);
                user.setRol(Usuario.Rol.registrado);
                usuarioRepository.save(user);
            }

            String jwt = jwtUtil.generateToken(user.getEmail(), user.getRol().name());
            String redirectUrl = frontendUrl + "/auth/callback?token=" + jwt + "&rol=" + user.getRol().name() + "&guid="
                    + user.getGuid() + "&nombre=" + URLEncoder.encode(user.getNombre(), StandardCharsets.UTF_8);

            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(frontendUrl + "/login?error=discord");
        }
    }
}
