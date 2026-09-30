package vn.iotstar.services;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import vn.iotstar.exception.ExpiredJwtException;
import vn.iotstar.exception.InvalidJwtSignatureException;

import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
    @Value("${security.jwt.secret-key}")
    private String secretKey;

    @Value("${security.jwt.expiration-time}")
    private long jwtExpiration;

    public String extractUsername(String token) {
        return extractClaim(token, JWTClaimsSet::getSubject);
    }

    public <T> T extractClaim(String token, Function<JWTClaimsSet, T> claimsResolver) {
        final JWTClaimsSet claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    public long getExpirationTime() {
        return jwtExpiration;
    }

    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {
        try {
            JWSSigner signer = new MACSigner(getSecretKeyBytes());

            JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                    .subject(userDetails.getUsername())
                    .issueTime(new Date(System.currentTimeMillis()))
                    .expirationTime(new Date(System.currentTimeMillis() + expiration));

            extraClaims.forEach(builder::claim);

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), builder.build());
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Error building Nimbus JWT token", e);
        }
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, JWTClaimsSet::getExpirationTime);
    }

    public JWTClaimsSet extractAllClaims(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(getSecretKeyBytes());

            if (!signedJWT.verify(verifier)) {
                throw new InvalidJwtSignatureException("The JWT signature is invalid");
            }

            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
            if (claimsSet.getExpirationTime() != null && claimsSet.getExpirationTime().before(new Date())) {
                throw new ExpiredJwtException("The JWT token has expired");
            }

            return claimsSet;
        } catch (InvalidJwtSignatureException | ExpiredJwtException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidJwtSignatureException("Invalid JWT token signature or format");
        }
    }

    private byte[] getSecretKeyBytes() {
        return Base64.getDecoder().decode(secretKey);
    }
}
