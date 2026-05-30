package com.example.nging.user.usecase.getcontact;

import com.example.nging.user.domain.getcontact.GetContactRequest;
import com.example.nging.user.domain.getcontact.GetContactResult;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GetContactUseCaseImpl implements GetContactUseCase {

    private final GetContactGateway gateway;

    @Override
    public void execute(GetContactRequest request, GetContactPresenter presenter) {
        gateway.findByUserId(request.userId())
                .ifPresentOrElse(
                        contact -> presenter.present(new GetContactResult(contact)),
                        () -> presenter.presentNotFound(request.userId())
                );
    }
}
