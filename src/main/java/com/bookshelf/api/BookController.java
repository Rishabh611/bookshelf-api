package com.bookshelf.api;

import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/books")
public class BookController {
    private final List<String> books = new ArrayList<>(
        List.of("Dune", "Clean Code", "The Paragmatic Programmer")
    );

    @GetMapping
    public List<String> getAllBooks() {
        return books;
    }

    @PostMapping
    public String addBook(@RequestBody String title) {
        books.add(title);
        return "Added: " + title;
    }
}
