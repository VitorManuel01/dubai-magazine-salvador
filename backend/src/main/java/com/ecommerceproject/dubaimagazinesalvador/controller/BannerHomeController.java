package com.ecommerceproject.dubaimagazinesalvador.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ecommerceproject.dubaimagazinesalvador.domain.vitrine.BannerHomeResponseDTO;
import com.ecommerceproject.dubaimagazinesalvador.services.vitrine.BannerHomeService;

@RestController
public class BannerHomeController {

    private final BannerHomeService bannerHomeService;

    public BannerHomeController(BannerHomeService bannerHomeService) {
        this.bannerHomeService = bannerHomeService;
    }

    @GetMapping("/banners-home")
    public List<BannerHomeResponseDTO> listar() {
        return bannerHomeService.listar();
    }

    @PutMapping(
            value = "/admin/banners-home/{posicao}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public BannerHomeResponseDTO atualizar(
            @PathVariable int posicao,
            @RequestParam("imagem") MultipartFile imagem
    ) {
        return bannerHomeService.atualizar(posicao, imagem);
    }
}
