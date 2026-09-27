package com.Mediatron.Lab_Management.data_dto;

import java.util.List;

public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private Integer id;
    private String email;
    private List<String> roles;
    private boolean delegationActive;

    public JwtResponse(String accessToken ,Integer id, String email , List<String> roles){
        this(accessToken, id, email, roles, false);
    }

    public JwtResponse(String accessToken ,Integer id, String email , List<String> roles, boolean delegationActive){
        this.token=accessToken;
        this.id= id;
        this.email=email;
        this.roles=roles;
        this.delegationActive = delegationActive;
    }

    public String getToken() {
        return token;
    }

    public String getType(){
        return type;
    }
    public Integer getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public List<String> getRoles() {
        return roles;
    }

    public boolean isDelegationActive() {
        return delegationActive;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public void setDelegationActive(boolean delegationActive) {
        this.delegationActive = delegationActive;
    }

}
