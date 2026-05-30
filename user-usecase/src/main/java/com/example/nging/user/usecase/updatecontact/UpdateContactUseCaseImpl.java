package com.example.nging.user.usecase.updatecontact;

import com.example.nging.user.domain.ContactRecord;
import com.example.nging.user.domain.updatecontact.UpdateContactRequest;
import com.example.nging.user.domain.updatecontact.UpdateContactResult;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UpdateContactUseCaseImpl implements UpdateContactUseCase {

    private final UpdateContactGateway gateway;

    @Override
    public void execute(UpdateContactRequest request, UpdateContactPresenter presenter) {
        gateway.updateByUserId(new ContactRecord(
                        null,
                        request.userId(),
                        request.phone(),
                        request.address(),
                        request.emergency()))
                .ifPresentOrElse(
                        contact -> presenter.present(new UpdateContactResult(contact)),
                        () -> presenter.presentNotFound(request.userId())
                );
    }
}
