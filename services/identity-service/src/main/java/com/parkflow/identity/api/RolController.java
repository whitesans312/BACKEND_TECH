package com.parkflow.identity.api;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.parkflow.identity.application.IdentityService;
@RestController @RequestMapping("/api/roles")
public class RolController {
    private final IdentityService service;
    public RolController(IdentityService service){this.service=service;}
    @GetMapping public List<RolResponse> all(){return service.findRoles();}
}
