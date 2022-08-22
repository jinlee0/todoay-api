package com.todoay.api.domain.todo.service;

import com.todoay.api.domain.auth.entity.Auth;
import com.todoay.api.domain.auth.repository.AuthRepository;
import com.todoay.api.domain.hashtag.repository.HashtagRepository;
import com.todoay.api.domain.todo.dto.DueDateTodoModifyRequestDto;
import com.todoay.api.domain.todo.dto.DueDateTodoReadResponseDto;
import com.todoay.api.domain.todo.dto.DueDateTodoSaveRequestDto;
import com.todoay.api.domain.todo.dto.DueDateTodoSaveResponseDto;
import com.todoay.api.domain.todo.entity.DueDateTodo;
import com.todoay.api.domain.todo.entity.Importance;
import com.todoay.api.domain.todo.exception.NotYourTodoException;
import com.todoay.api.domain.todo.exception.TodoNotFoundException;
import com.todoay.api.domain.todo.repository.DueDateTodoRepository;
import com.todoay.api.domain.todo.utility.EnumTransformer;
import com.todoay.api.domain.todo.utility.HashtagAttacher;
import com.todoay.api.global.context.LoginAuthContext;
import com.todoay.api.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DueDateTodoCRUDServiceImpl implements DueDateTodoCRUDService {
    private final DueDateTodoRepository dueDateTodoRepository;
    private final AuthRepository authRepository;
    private final JwtProvider jwtProvider;

    private final LoginAuthContext loginAuthContext;
    private final HashtagRepository hashtagRepository;


    @Override
    public DueDateTodoSaveResponseDto addTodo(DueDateTodoSaveRequestDto dto) {
        Auth auth = authRepository.findByEmail(jwtProvider.getLoginId()).get();
        DueDateTodo dueDateTodo = DueDateTodo.builder()
                .title(dto.getTitle())
                .isPublic(dto.isPublic())
                .isFinished(false)
                .dueDate(dto.getDueDate())
                .description(dto.getDescription())
                .importance((Importance) EnumTransformer.valueOfEnum(Importance.class,dto.getImportance()))
                .auth(auth)
                .build();

        HashtagAttacher.attachHashtag(dueDateTodo, dto.getHashtagNames(), hashtagRepository);
        dueDateTodoRepository.save(dueDateTodo);
        return DueDateTodoSaveResponseDto.builder().id(dueDateTodo.getId()).build();
    }

    @Override
    @Transactional
    public void modifyDueDateTodo(Long id, DueDateTodoModifyRequestDto dto) {
        DueDateTodo dueDateTodo = checkIsPresentAndIsMineAndGetTodo(id);
        dueDateTodo.modify(dto.getTitle(), dto.isPublic(),dto.isFinished(), dto.getDueDate(), dto.getDescription(),
                (Importance) EnumTransformer.valueOfEnum(Importance.class,dto.getImportance()));
        HashtagAttacher.attachHashtag(dueDateTodo, dto.getHashtagNames(), hashtagRepository);
    }

    @Transactional
    public void deleteDueDateTodo(Long id) {
        DueDateTodo dueDateTodo = checkIsPresentAndIsMineAndGetTodo(id);
        dueDateTodoRepository.delete(dueDateTodo);
    }

    @Override
    public List<DueDateTodoReadResponseDto> readTodosOrderByDueDate() {
        Auth loginedAuth = loginAuthContext.getLoginAuth();
        List<DueDateTodo> todos = dueDateTodoRepository.findAllByAuth(loginedAuth);
        return sortDueDateTodoByDueDate(todos);
    }

    @Override
    public List<DueDateTodoReadResponseDto> readTodosOrderByImportance() {
        Auth loginedAuth = loginAuthContext.getLoginAuth();
        List<DueDateTodo> todos = dueDateTodoRepository.findAllByAuth(loginedAuth);
        return sortDueDateTodoByImportance(todos);
    }

    private DueDateTodo checkIsPresentAndIsMineAndGetTodo(Long id) {
        DueDateTodo dueDateTodo = checkIsPresentAndGetTodo(id);
        checkThisTodoIsMine(dueDateTodo);
        return dueDateTodo;
    }

    private void checkThisTodoIsMine(DueDateTodo dueDateTodo) {
        if(!dueDateTodo.getAuth().equals(loginAuthContext.getLoginAuth()))throw new NotYourTodoException();
    }

    private DueDateTodo checkIsPresentAndGetTodo(Long id) {
        return dueDateTodoRepository.findById(id).orElseThrow(TodoNotFoundException::new);
    }

    private List<DueDateTodoReadResponseDto> sortDueDateTodoByDueDate(List<DueDateTodo> todos) {
        return todos.stream()
                .sorted(Comparator.comparing(DueDateTodo::getDueDate))
                .map(DueDateTodoReadResponseDto::createDto)
                .collect(Collectors.toList());
    }

    private List<DueDateTodoReadResponseDto> sortDueDateTodoByImportance(List<DueDateTodo> todos){
        return todos.stream()
                .sorted(Comparator.comparing(DueDateTodo::getImportance))
                .map(DueDateTodoReadResponseDto::createDto)
                .collect(Collectors.toList());
    }
}
