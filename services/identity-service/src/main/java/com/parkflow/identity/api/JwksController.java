package com.parkflow.identity.api;

import java.util.Map;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JwksController {
    private final RSAKey key;
    public JwksController(RSAKey key){this.key=key;}
    @GetMapping("/.well-known/jwks.json") public Map<String,Object> jwks(){return new JWKSet(key.toPublicJWK()).toJSONObject();}
}
