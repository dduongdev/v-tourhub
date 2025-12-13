package com.v_tourhub.catalog_service.service;

import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.v_tourhub.catalog_service.entity.Destination;
import com.v_tourhub.catalog_service.entity.Media;
import com.v_tourhub.catalog_service.entity.TourismService;
import com.v_tourhub.catalog_service.repository.DestinationRepository;
import com.v_tourhub.catalog_service.repository.MediaRepository;
import com.v_tourhub.catalog_service.repository.TourismServiceRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {

    private final MinioClient minioClient;
    private final MediaRepository mediaRepo;
    private final DestinationRepository destRepo;
    private final TourismServiceRepository serviceRepo;

    @Value("${minio.bucket}")
    private String bucketName;

    @Value("${minio.external-url}")
    private String externalUrl;

    private String uploadToMinio(MultipartFile file) {
        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                String policy = """
                        {
                          "Version": "2012-10-17",
                          "Statement": [
                            {
                              "Effect": "Allow",
                              "Principal": {"AWS": ["*"]},
                              "Action": ["s3:GetObject"],
                              "Resource": ["arn:aws:s3:::%s/*"]
                            }
                          ]
                        }
                        """.formatted(bucketName);
                minioClient.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucketName).config(policy).build());
            }

            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());

            return fileName;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi upload file: " + e.getMessage());
        }
    }

    public String getFileUrl(String fileName) {
        return String.format("%s/%s/%s", externalUrl, bucketName, fileName);
    }

    @Transactional
    public Media addMediaToDestination(Long destId, MultipartFile file, String caption) {
        Destination dest = destRepo.findById(destId)
                .orElseThrow(() -> new RuntimeException("Destination not found"));

        String fileName = uploadToMinio(file);

        Media media = Media.builder()
                .url(fileName)
                .type(file.getContentType())
                .caption(caption)
                .destination(dest)
                .build();

        return mediaRepo.save(media);
    }

    @Transactional
    public Media addMediaToService(Long serviceId, MultipartFile file, String caption) {
        TourismService service = serviceRepo.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        String fileName = uploadToMinio(file);

        Media media = Media.builder()
                .url(fileName)
                .type(file.getContentType())
                .caption(caption)
                .tourismService(service)
                .build();

        return mediaRepo.save(media);
    }

    @Transactional
    public List<String> addBatchMediaToDestination(Long destId, List<MultipartFile> files) {
        Destination dest = destRepo.findById(destId)
                .orElseThrow(() -> new RuntimeException("Destination not found"));

        List<Media> mediaList = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty())
                continue;

            String fileName = uploadToMinio(file);

            Media media = Media.builder()
                    .url(fileName)
                    .type(file.getContentType())
                    .caption(file.getOriginalFilename())
                    .destination(dest)
                    .build();

            mediaList.add(media);
        }

        mediaRepo.saveAll(mediaList);

        return mediaList.stream()
                .map(m -> getFileUrl(m.getUrl()))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<String> addBatchMediaToService(Long serviceId, List<MultipartFile> files) {
        TourismService service = serviceRepo.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        List<Media> mediaList = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty())
                continue;

            String fileName = uploadToMinio(file);

            Media media = Media.builder()
                    .url(fileName)
                    .type(file.getContentType())
                    .caption(file.getOriginalFilename())
                    .tourismService(service)
                    .build();

            mediaList.add(media);
        }

        mediaRepo.saveAll(mediaList);

        return mediaList.stream()
                .map(m -> getFileUrl(m.getUrl()))
                .collect(Collectors.toList());
    }
}
