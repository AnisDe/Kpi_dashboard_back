package com.example.demo.controller;

import com.example.demo.service.DatabaseMetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/metadata")
public class DatabaseMetadataController {

    @Autowired
    private DatabaseMetadataService databaseMetadataService;

    /*
        @GetMapping("/tables")
        public List<TableMetadata> getTables() throws SQLException {
            return databaseMetadataService.getTables();
        }*/
    @GetMapping("/connect")
    public String showConnectionForm() {

        return "connectionForm";
    }


}
