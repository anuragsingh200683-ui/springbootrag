package com.example.employeemanagement.designation.service;

import com.example.employeemanagement.common.exception.DuplicateResourceException;
import com.example.employeemanagement.common.exception.ResourceNotFoundException;
import com.example.employeemanagement.designation.dto.DesignationRequest;
import com.example.employeemanagement.designation.dto.DesignationResponse;
import com.example.employeemanagement.designation.entity.Designation;
import com.example.employeemanagement.designation.mapper.DesignationMapper;
import com.example.employeemanagement.designation.repository.DesignationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DesignationServiceImpl implements DesignationService {

    private static final Logger log = LoggerFactory.getLogger(DesignationServiceImpl.class);

    private final DesignationRepository designationRepository;
    private final DesignationMapper designationMapper;

    @Override
    public DesignationResponse createDesignation(DesignationRequest request) {
        if (designationRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException(
                    "A designation named '" + request.getName() + "' already exists");
        }
        Designation saved = designationRepository.save(designationMapper.toEntity(request));
        log.info("Created designation id={} name={}", saved.getId(), saved.getName());
        return designationMapper.toResponse(saved);
    }

    @Override
    public DesignationResponse updateDesignation(Long id, DesignationRequest request) {
        Designation designation = designationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forId("Designation", id));

        if (designationRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw new DuplicateResourceException(
                    "A designation named '" + request.getName() + "' already exists");
        }

        designationMapper.updateEntity(designation, request);
        Designation saved = designationRepository.save(designation);
        log.info("Updated designation id={}", saved.getId());
        return designationMapper.toResponse(saved);
    }

    @Override
    public void deleteDesignation(Long id) {
        if (!designationRepository.existsById(id)) {
            throw ResourceNotFoundException.forId("Designation", id);
        }
        designationRepository.deleteById(id);
        log.info("Deleted designation id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public DesignationResponse getDesignationById(Long id) {
        Designation designation = designationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forId("Designation", id));
        return designationMapper.toResponse(designation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DesignationResponse> getAllDesignations() {
        return designationRepository.findAll().stream()
                .map(designationMapper::toResponse)
                .toList();
    }
}
