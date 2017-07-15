package gov.samhsa.c2s.ums.service;

import gov.samhsa.c2s.ums.domain.User;
import gov.samhsa.c2s.ums.domain.UserAvatar;
import gov.samhsa.c2s.ums.domain.UserAvatarRepository;
import gov.samhsa.c2s.ums.domain.UserRepository;
import gov.samhsa.c2s.ums.service.dto.AvatarBytesAndMetaDto;
import gov.samhsa.c2s.ums.service.dto.UserAvatarDto;
import gov.samhsa.c2s.ums.service.exception.InvalidAvatarInputException;
import gov.samhsa.c2s.ums.service.exception.UserAvatarDeleteException;
import gov.samhsa.c2s.ums.service.exception.UserAvatarNotFoundException;
import gov.samhsa.c2s.ums.service.exception.UserAvatarSaveException;
import gov.samhsa.c2s.ums.service.exception.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class UserAvatarServiceImpl implements UserAvatarService {
    private static final Long REQUIRED_WIDTH_IN_PIXELS = 460L;  // TODO: Replace this hardcoded constant with externalized configurable value
    private static final Long REQUIRED_HEIGHT_IN_PIXELS = 460L;  // TODO: Replace this hardcoded constant with externalized configurable value

    private final ModelMapper modelMapper;
    private final UserAvatarRepository userAvatarRepository;
    private final UserRepository userRepository;

    @Autowired
    public UserAvatarServiceImpl(ModelMapper modelMapper, UserAvatarRepository userAvatarRepository, UserRepository userRepository) {
        this.modelMapper = modelMapper;
        this.userAvatarRepository = userAvatarRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserAvatarDto getUserAvatarByUserId(Long userId) {
        return modelMapper.map(
                userAvatarRepository.findByUserId(userId)
                        .orElseThrow(UserAvatarNotFoundException::new),
                UserAvatarDto.class
        );
    }

    @Override
    @Transactional
    public UserAvatarDto saveUserAvatar(Long userId, AvatarBytesAndMetaDto avatarFile, Long fileWidthPixels, Long fileHeightPixels) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User Not Found!"));

        UserAvatar savedUserAvatar;

        try {
            savedUserAvatar = userAvatarRepository.save(buildNewUserAvatar(avatarFile, fileWidthPixels, fileHeightPixels, user));
        } catch (RuntimeException e) {
            log.error("A RuntimeException occurred while attempting to save a new user avatar", e);
            throw new UserAvatarSaveException("Unable to save user avatar");
        }

        return modelMapper.map(savedUserAvatar, UserAvatarDto.class);
    }

    @Override
    @Transactional
    public UserAvatarDto updateUserAvatar(Long userId, AvatarBytesAndMetaDto avatarFile, Long fileWidthPixels, Long fileHeightPixels) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User Not Found!"));

        UserAvatar userAvatar = userAvatarRepository.findByUserId(user.getId())
                .orElseThrow(() -> new UserAvatarNotFoundException("The user does not have an existing avatar to update"));

        userAvatar.setFileContents(avatarFile.getFileContents());
        userAvatar.setFileExtension(avatarFile.getFileExtension());
        userAvatar.setFileName(avatarFile.getFileName());
        userAvatar.setFileSizeBytes(avatarFile.getFileSizeBytes());
        userAvatar.setFileHeightPixels(fileHeightPixels);
        userAvatar.setFileWidthPixels(fileWidthPixels);

        UserAvatar savedUserAvatar;

        try {
            savedUserAvatar = userAvatarRepository.save(userAvatar);
        } catch (RuntimeException e) {
            log.error("A RuntimeException occurred while attempting to update a user avatar", e);
            throw new UserAvatarSaveException("Unable to update user avatar");
        }

        return modelMapper.map(savedUserAvatar, UserAvatarDto.class);
    }

    @Override
    @Transactional
    public void deleteUserAvatar(Long userId) {
        try {
            userAvatarRepository.deleteByUserId(userId);
        } catch (RuntimeException e) {
            log.error("A RuntimeException occurred while attempting to delete a user's avatar", e);
            throw new UserAvatarDeleteException("Unable to delete user's avatar");
        }
    }

    private UserAvatar buildNewUserAvatar(AvatarBytesAndMetaDto avatarFile, Long fileWidthPixels, Long fileHeightPixels, User user) {
        if (avatarFile.getFileContents() == null || avatarFile.getFileContents().length <= 0) {
            log.error("Unable to generate a new UserAvatar object in buildNewUserAvatar method because value of avatarFile.getFileContents is null or the length is less than or equal to zero");
            throw new InvalidAvatarInputException("The avatar file cannot be null");
        }

        // TODO: Add check to ensure file extension is one of the permitted types

        // TODO: Add check to ensure file size is equal or less than configured max file size

        if (fileWidthPixels == null || !fileWidthPixels.equals(REQUIRED_WIDTH_IN_PIXELS)) {
            log.error("Unable to generate a new UserAvatar object in buildNewUserAvatar method because value of fileWidthPixels parameter is null or not equal to required value (" + REQUIRED_WIDTH_IN_PIXELS + "):", fileWidthPixels);
            throw new InvalidAvatarInputException("The avatar file image width is not valid");
        }

        if (fileHeightPixels == null || !fileHeightPixels.equals(REQUIRED_HEIGHT_IN_PIXELS)) {
            log.error("Unable to generate a new UserAvatar object in buildNewUserAvatar method because value of fileHeightPixels parameter is null or not equal to required value (" + REQUIRED_HEIGHT_IN_PIXELS + "):", fileHeightPixels);
            throw new InvalidAvatarInputException("The avatar file image height is not valid");
        }

        UserAvatar newUserAvatar = new UserAvatar();
        newUserAvatar.setFileContents(avatarFile.getFileContents());
        newUserAvatar.setFileExtension(avatarFile.getFileExtension());
        newUserAvatar.setFileName(avatarFile.getFileName());
        newUserAvatar.setFileSizeBytes(avatarFile.getFileSizeBytes());
        newUserAvatar.setFileHeightPixels(fileHeightPixels);
        newUserAvatar.setFileWidthPixels(fileWidthPixels);
        newUserAvatar.setUser(user);

        return newUserAvatar;
    }
}
