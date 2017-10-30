package test.envoy.books;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

class BooksRepository {

    //Just hardcoding a book
    JsonArray findAllBooks() {
        JsonArray books = new JsonArray();
        books.add(findAnyBook());

        return books;
    }

    JsonObject findAnyBook() {
        JsonObject book = new JsonObject();
        book.put("id", "1").put("name", "The Mystic Eye").put("author", "Sadhguru");
        return book;
    }
}
