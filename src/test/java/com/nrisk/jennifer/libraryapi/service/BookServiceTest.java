package com.nrisk.jennifer.libraryapi.service;

import com.nrisk.jennifer.libraryapi.exception.BusinessException;
import com.nrisk.jennifer.libraryapi.model.entity.Book;
import com.nrisk.jennifer.libraryapi.model.repository.BookRepository;
import com.nrisk.jennifer.libraryapi.service.impl.BookServiceImpl;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test") //apenas testes unitarios
public class BookServiceTest {

    BookService service;
    @MockBean
    BookRepository repository;

    @BeforeEach //faz com que o metodo a seguir seja executado antes de cada teste da classe
    public void setUp() {
        this.service = new BookServiceImpl(repository);
    }


    @Test
    @DisplayName("Deve salvar um livro")
    public void saveBookTest() {
        //cenario
        Book book = createValidBook(); //criando uma instancia de Book
        Mockito.when(repository.existsByIsbn(Mockito.anyString())).thenReturn(false); //quando executar o metodo existsByIsbn do repository, para qualquer string, retorna false

        Mockito.when(repository.save(book)).thenReturn( //simula o comportamento do repository. Vai passar pra save o objeto book e quando ele salvar especificamente a instancia book, ele vai retornar o objeto abaixo
                Book.builder().id(1l)
                        .isbn("123")
                        .author("Fulano")
                        .title("As aventuras").build()
        );


        //execucao
        Book savedBook = service.save(book); //salva o objeto book

        //verificacao
        assertThat(savedBook.getId()).isNotNull();
        assertThat(savedBook.getIsbn()).isEqualTo("123");
        assertThat(savedBook.getTitle()).isEqualTo("As aventuras");
        assertThat(savedBook.getAuthor()).isEqualTo("Fulano");
    }

    private static Book createValidBook() {
        return Book.builder().isbn("123").author("Fulano").title("As aventuras").build();
    }

    @Test
    @DisplayName("Deve lançar erro de negocio ao tentar salvar um livro com isbn duplicado")
    public void shouldNotSaveABookWithDuplicatedISBN() {
        //cenario
        Book book = createValidBook();
        Mockito.when(repository.existsByIsbn(Mockito.anyString())).thenReturn(true); //quando executar o metodo existsByIsbn do repository, para qualquer string, retorna true
        //execucao
        Throwable exception = Assertions.catchThrowable(() -> service.save(book));//quando executar o save, ele vai lancar uma exception e salva-la na variavel exception do tipo Throwable

        //verificacoes
        assertThat(exception) //vamos verificar na exception
                .isInstanceOf(BusinessException.class) //se ela é uma instancia da classe BusinessException, que é a exception que ela deve ser
                .hasMessage("Isbn ja cadastrado");     //e se a mensagem dessa exception é "Isbn ja cadastrado"

        //temos que proibir de chamar o save do repository, pois mesmo lancando erro,ele chama o save
        Mockito.verify(repository, Mockito.never()).save(book); //vai verificar que o repository nunca vai executar o metodo save com o parametro book
    }

    @Test
    @DisplayName("Deve obter um livro por id")
    public void getByIdTest() {
        Long id = 1l;
        Book book = createValidBook();
        book.setId(id);
        Mockito.when(repository.findById(id)).thenReturn(Optional.of(book));

        //execucao
        Optional<Book> foundBook = service.getById(id);

        //verificacoes
        assertThat(foundBook.isPresent()).isTrue();
        assertThat(foundBook.get().getId()).isEqualTo(id);
        assertThat(foundBook.get().getAuthor()).isEqualTo(book.getAuthor());
        assertThat(foundBook.get().getTitle()).isEqualTo(book.getTitle());
        assertThat(foundBook.get().getIsbn()).isEqualTo(book.getIsbn());


    }

    @Test
    @DisplayName("Deve retornar vazio ao obter um livro por id quando ele nao existe na base")
    public void bookNotFoundByIdTest() {
        Long id = 1l;
        Mockito.when(repository.findById(id)).thenReturn(Optional.empty()); //estamos mandando ele retornar vazio quando ele simular um findById

        //execucao
        Optional<Book> book = service.getById(id);

        //verificacoes
        assertThat(book.isPresent()).isFalse();

    }

    @Test
    @DisplayName("Deve deletar um livro por id")
    public void deleteBookTest() {
        Long id = 1l;
        Book book = createValidBook();
        book.setId(id);

        //execucao
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> service.delete(book)); //verifique que ele nao lance nada, nenhum erro

        //verificacoes
        Mockito.verify(repository, Mockito.times(1)).delete(book); //verifico se o repository chamou o metodo delete somente 1 vez com especificamente o parametro book criado acima

    }

    @Test
    @DisplayName("Deve ocorrer erro ao tentar deletar um livro inexistente")
    public void deleteInvalidBookTest() {
        Book book = new Book();

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> service.delete(book));

        //verificacoes
        Mockito.verify(repository, Mockito.never()).delete(book); //verifico se o repository nunca chamou o metodo delete com especificamente o parametro book criado acima

    }

    @Test
    @DisplayName("Deve ocorrer erro ao tentar atualizar um livro inexistente")
    public void updateInvalidBookTest() {
        Book book = new Book();

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> service.update(book));

        //verificacoes
        Mockito.verify(repository, Mockito.never()).save(book); //verifico se o repository nunca chamou o metodo save(que tambem atualiza) com especificamente o parametro book criado acima

    }

    @Test
    @DisplayName("Deve atualizar um livro")
    public void updateBookTest() {
        //cenario
        long id = 1l;

        //livro a atualizar
        Book updatingBook = Book.builder().id(id).build(); //criando uma instancia de Book

        //simulacao
        Book updatedBook = createValidBook(); //criando outro livro, vai simular o livro acima atualizado
        updatedBook.setId(id);

        Mockito.when(repository.save(updatingBook)).thenReturn(updatedBook);

        //execucao
        Book book = service.update(updatingBook);

        //verificacao
        assertThat(book.getId()).isEqualTo(updatedBook.getId());
        assertThat(book.getTitle()).isEqualTo(updatedBook.getTitle());
        assertThat(book.getAuthor()).isEqualTo(updatedBook.getAuthor());
        assertThat(book.getIsbn()).isEqualTo(updatedBook.getIsbn());

    }

    @Test
    @DisplayName("Deve filtrar livros pelas propriedades")
    public void findBookTest(){

        Book book = createValidBook();

        PageRequest pageRequest = PageRequest.of(0, 10);
        List<Book> lista = Arrays.asList(book);
        Page<Book> page = new PageImpl<Book>(lista, pageRequest, 1);
        Mockito.when(repository.findAll(Mockito.any(Example.class), Mockito.any(PageRequest.class)))
                .thenReturn(page);

        Page<Book> result = service.find(book, pageRequest);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).isEqualTo(lista);
        assertThat(result.getPageable().getPageNumber()).isEqualTo(0);
        assertThat(result.getPageable().getPageSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("Deve obter um livro pelo isbn")
    public void getBookByIsbnTest(){
        String isbn = "1230";

        Mockito.when(repository.findByIsbn(isbn)).thenReturn(Optional.of(Book.builder().id(1l).isbn(isbn).build()));  //quando procurar na base o livro, vamos simular que ele retornou o livro

        Optional<Book> book = service.getBookByIsbn(isbn);

        assertThat(book.isPresent()).isTrue(); //verifica se ha um livro
        assertThat(book.get().getId()).isEqualTo(1l); //verifica se o id de book é igual a 1
        assertThat(book.get().getIsbn()).isEqualTo(isbn); //verifica se o isbn de book é igual a  isbn do parametro

        Mockito.verify(repository, Mockito.times(1)).findByIsbn(isbn); //verifica se repository chamou o metodo findByIsbn uma vez
    }
}
