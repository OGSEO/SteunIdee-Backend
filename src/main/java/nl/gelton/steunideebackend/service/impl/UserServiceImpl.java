package nl.gelton.steunideebackend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.gelton.steunideebackend.dto.LoginRequest;
import nl.gelton.steunideebackend.dto.Response;
import nl.gelton.steunideebackend.dto.input.UserInputDto;
import nl.gelton.steunideebackend.dto.mapper.UserMapper;
import nl.gelton.steunideebackend.dto.output.UserOutputDto;
import nl.gelton.steunideebackend.enums.UserRole;
import nl.gelton.steunideebackend.exception.InvalidCredentialsException;
import nl.gelton.steunideebackend.model.User;
import nl.gelton.steunideebackend.repository.FileUploadRepository;
import nl.gelton.steunideebackend.repository.UserRepository;
import nl.gelton.steunideebackend.security.jwt.JwtUtils;
import nl.gelton.steunideebackend.service.interf.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final FileUploadRepository fileUploadRepository;
    private final AvatarService avatarService;

    @Override
    public Response registerUser(UserInputDto registrationRequest) {
        UserRole role = UserRole.CITIZEN;

        if (registrationRequest.getRole() != null && registrationRequest.getRole().equals("politician")) {
            role = UserRole.POLITICIAN;
        }
        User user = User.builder()
                .name(registrationRequest.getName())
                .email(registrationRequest.getEmail())
                .password(passwordEncoder.encode(registrationRequest.getPassword()))
                .role(registrationRequest.getRole())
                .build();

        User savedUser = userRepository.save(user);
        UserOutputDto userOutputDto = UserMapper.fromModelToOutputDto(savedUser);

        return Response.builder()
                .statusCode(200)
                .statusMessage("User registered successfully")
                .user(userOutputDto)
                .build();
    }

    @Override
    public Response loginUser(LoginRequest loginRequest) {

        User user = userRepository.findByEmail(loginRequest.getEmail()).orElseThrow(()-> new UsernameNotFoundException("Email not found"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Password incorrect");
        }
        String token = jwtUtils.generateToken(user);

        return Response.builder()
                .statusCode(200)
                .statusMessage("User logged in successfully")
                .token(token)
                .expirationTime("1 Week")
                .role(user.getRole().name())
                .build();
    }

    @Override
    public Response getAllUsers() {
        List<User> users = userRepository.findAll();
        List<UserOutputDto> allUserOutputDtos = new ArrayList<>();
        for (User user : users) {
            allUserOutputDtos.add(UserMapper.fromModelToOutputDto(user));
        }
        return Response.builder()
                .statusCode(200)
                .statusMessage("All users found")
                .userList(allUserOutputDtos)
                .build();
    }

    @Override
    public User getLoggedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        log.info("User Email is: {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(()-> new UsernameNotFoundException("User Not Found"));
    }

    @Override
    public Response getUserInfoWithAddress() {
        User user = getLoggedUser();
        UserOutputDto userOutputDto = UserMapper.fromModelToOutputDtoPlusAddress(user);

        return Response.builder()
                .statusCode(200)
                .statusMessage("User info with address found")
                .user(userOutputDto)
                .build();
    }


//    @Transactional
//    public Resource getAvatarFromUser(Long userId){
//        Optional<User> optionalUser = userRepository.findById(userId);
//        if(optionalUser.isEmpty()){
//            throw new RecordNotFoundException("User with id " + userId + " not found.");
//        }
//        ProfileImage avatar = optionalUser.get().getProfileImage();
//        if(avatar == null){
//            throw new RecordNotFoundException("User " + userId + " had no avatar.");
//        }
//        return avatarService.downLoadFile(avatar.getFileName());
//    }

//    @Transactional
//    public User assignAvatarToUser(String filename, Long userId) {
//        Optional<User> optionalUser = userRepository.findById(userId);
//        Optional<ProfileImage> optionalAvatar = fileUploadRepository.findByFileName(filename);
//
//        if (optionalUser.isPresent() && optionalAvatar.isPresent()) {
//            ProfileImage avatar = optionalAvatar.get();
//            User user = optionalUser.get();
//            user.setProfileImage(avatar);
//            return userRepository.save(user);
//        } else {
//            throw new RecordNotFoundException("user of avatar niet gevonden");
//        }
//    }
}

