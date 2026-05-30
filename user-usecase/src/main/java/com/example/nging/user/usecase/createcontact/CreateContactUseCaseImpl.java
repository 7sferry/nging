package com.example.nging.user.usecase.createcontact;

import com.example.nging.user.domain.ContactRecord;
import com.example.nging.user.domain.createcontact.CreateContactRequest;
import com.example.nging.user.domain.createcontact.CreateContactResult;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateContactUseCaseImpl implements CreateContactUseCase {

    private final CreateContactGateway gateway;

    @Override
    public void execute(CreateContactRequest request, CreateContactPresenter presenter) {
        ContactRecord saved = gateway.save(new ContactRecord(
                null,
                request.userId(),
                request.phone(),
                request.address(),
                request.emergency()
        ));
        presenter.present(new CreateContactResult(saved));
    }
}
