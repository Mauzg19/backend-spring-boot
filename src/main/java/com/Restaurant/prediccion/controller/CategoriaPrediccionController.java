package com.Restaurant.prediccion.controller;

import com.Restaurant.Exception.UserException;
import com.Restaurant.domain.USER_ROLE;
import com.Restaurant.model.User;
import com.Restaurant.prediccion.model.CategoriaPrediccionRequest;
import com.Restaurant.prediccion.model.CategoriaPrediccionResponse;
import com.Restaurant.prediccion.model.PrediccionEntity;
import com.Restaurant.prediccion.service.CategoriaPrediccionService;
import com.Restaurant.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categoria")
public class CategoriaPrediccionController {
    
    record PrediccionArchiveRequest(boolean archived) {}

    @Autowired
    private CategoriaPrediccionService categoriaPrediccionService;

    @Autowired
    private UserService userService;

    private void checkAdmin(String authHeader) throws Exception {
        if (authHeader == null) throw new UserException("Authorization header missing");
        User user = userService.findUserProfileByJwt(authHeader);
        if (user == null || (user.getRole() != USER_ROLE.ROLE_ADMIN && user.getRole() != USER_ROLE.ROLE_RESTAURANT_OWNER)) {
            throw new UserException("Access denied: admin or restaurant owner role required");
        }
    }

    @PostMapping("/prediccion")
    public ResponseEntity<CategoriaPrediccionResponse> predecirCategoria(@Valid @RequestBody CategoriaPrediccionRequest request, @RequestHeader("Authorization") String authHeader) throws Exception {
        checkAdmin(authHeader);
        CategoriaPrediccionResponse resp = categoriaPrediccionService.predecirCategoria(request);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/predicciones")
    public ResponseEntity<List<PrediccionEntity>> obtenerTodasLasPredicciones(@RequestHeader("Authorization") String authHeader) throws Exception {
        checkAdmin(authHeader);
        return ResponseEntity.ok(categoriaPrediccionService.obtenerTodasLasPredicciones());
    }

    @PatchMapping("/predicciones/{id}")
    public ResponseEntity<PrediccionEntity> actualizarEstadoPrediccion(
            @PathVariable Long id,
            @RequestBody PrediccionArchiveRequest request,
            @RequestHeader("Authorization") String authHeader) throws Exception {
        checkAdmin(authHeader);
        PrediccionEntity updated = categoriaPrediccionService.actualizarEstadoPrediccion(id, request.archived());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/predicciones/{id}")
    public ResponseEntity<?> eliminarPrediccion(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) throws Exception {
        checkAdmin(authHeader);
        categoriaPrediccionService.eliminarPrediccion(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<PrediccionEntity>> buscarPredicciones(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) Double price,
            @RequestParam(required = false) Boolean isVegetarian,
            @RequestParam(required = false) Boolean isSeasonal,
            @RequestParam(required = false) Integer quantity,
            @RequestParam(required = false) String predictedCategory) throws Exception {

        checkAdmin(authHeader);
        return ResponseEntity.ok(categoriaPrediccionService.buscarPredicciones(price, isVegetarian, isSeasonal, quantity, predictedCategory));
    }

}
