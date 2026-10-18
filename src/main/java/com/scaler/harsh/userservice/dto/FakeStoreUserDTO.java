package com.scaler.harsh.userservice.dto;

import com.scaler.harsh.userservice.model.Address;
import com.scaler.harsh.userservice.model.Name;
import com.scaler.harsh.userservice.model.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FakeStoreUserDTO {

    private int id;
    private String email;
    private String username;
    private String password;
    private Name name;
    private Address address;
    private String phone;


    public User toUser() {
        User user = new User();
        user.setId(this.id);
        user.setEmail(this.email);
        user.setUsername(this.username);
        user.setPassword(this.password);
        user.setAddress(this.address);
        user.setPhone(this.phone);

        return user;
    }

    public static FakeStoreUserDTO fromUser(CreateUserRequestDTO request) {
        FakeStoreUserDTO dto = new FakeStoreUserDTO();
        dto.setEmail(request.getEmail());
        dto.setUsername(request.getUsername());
        dto.setPassword(request.getPassword());
        dto.setAddress(request.getAddress());
        dto.setPhone(request.getPhone());
        dto.setName(request.getName());

        return dto;
    }
}
