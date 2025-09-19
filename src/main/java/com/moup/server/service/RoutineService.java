package com.moup.server.service;

import com.moup.server.repository.RoutineRepository;
import com.moup.server.repository.RoutineTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoutineService {
    private final RoutineRepository routineRepository;
    private final RoutineTaskRepository routineTaskRepository;

}
