
package com.example.gerenciadorlivros;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private EditText edtTitulo, edtAutor, edtAno, edtBusca;
    private Spinner spGenero, spStatus, spFiltroGenero;
    private CheckBox chkFavorito;
    private Button btnSalvar;
    private ListView listaLivros;
    private TextView txtQuantidade;

    private FirebaseFirestore db;
    private CollectionReference livrosRef;

    private ArrayList<String> generos;
    private ArrayList<String> status;
    private ArrayList<String> filtros;

    private ArrayList<Livro> todosLivros;
    private ArrayList<String> todosIds;

    private ArrayList<String> listaNomes;
    private ArrayList<String> idsExibidos;
    private ArrayList<Livro> livrosExibidos;

    private ArrayAdapter<String> adapterLista;

    private String idLivroEdicao = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Localizar os componentes da tela
        edtTitulo = findViewById(R.id.edtTitulo);
        edtAutor = findViewById(R.id.edtAutor);
        edtAno = findViewById(R.id.edtAno);
        edtBusca = findViewById(R.id.edtBusca);

        spGenero = findViewById(R.id.spGenero);
        spStatus = findViewById(R.id.spStatus);
        spFiltroGenero = findViewById(R.id.spFiltroGenero);

        chkFavorito = findViewById(R.id.chkFavorito);
        btnSalvar = findViewById(R.id.btnSalvar);
        listaLivros = findViewById(R.id.listaLivros);
        txtQuantidade = findViewById(R.id.txtQuantidade);

        // Criar as listas
        todosLivros = new ArrayList<>();
        todosIds = new ArrayList<>();

        listaNomes = new ArrayList<>();
        idsExibidos = new ArrayList<>();
        livrosExibidos = new ArrayList<>();

        // Configurar os Spinners
        configurarSpinners();

        // Configurar a lista
        adapterLista = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                listaNomes
        );

        listaLivros.setAdapter(adapterLista);

        // Conectar ao Firebase
        db = FirebaseFirestore.getInstance();
        livrosRef = db.collection("livros");

        // Carregar os livros
        listarLivros();

        // Botão salvar
        btnSalvar.setOnClickListener(v -> salvarLivro());

        // Clique curto para editar
        listaLivros.setOnItemClickListener((parent, view, position, id) -> {
            editarLivro(position);
        });

        // Clique longo para excluir
        listaLivros.setOnItemLongClickListener((parent, view, position, id) -> {
            confirmarExclusao(position);
            return true;
        });

        // Busca por título
        edtBusca.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                atualizarLista();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Filtro por gênero
        spFiltroGenero.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(AdapterView<?> parent,
                                               View view,
                                               int position, long id) {
                        atualizarLista();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                }
        );
    }

    // Configurar os Spinners
    private void configurarSpinners() {

        generos = new ArrayList<>();

        generos.add("Selecione o gênero");
        generos.add("Romance");
        generos.add("Fantasia");
        generos.add("Terror");
        generos.add("Ficção Científica");
        generos.add("Biografia");

        status = new ArrayList<>();

        status.add("Selecione o status");
        status.add("Quero ler");
        status.add("Lendo");
        status.add("Concluído");

        filtros = new ArrayList<>();

        filtros.add("Todos os gêneros");
        filtros.addAll(generos.subList(1, generos.size()));

        ArrayAdapter<String> adapterGenero = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                generos
        );

        ArrayAdapter<String> adapterStatus = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                status
        );

        ArrayAdapter<String> adapterFiltro = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                filtros
        );

        adapterGenero.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        adapterStatus.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        adapterFiltro.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spGenero.setAdapter(adapterGenero);
        spStatus.setAdapter(adapterStatus);
        spFiltroGenero.setAdapter(adapterFiltro);
    }

    // Salvar ou atualizar livro
    private void salvarLivro() {

        String titulo = edtTitulo.getText().toString().trim();
        String autor = edtAutor.getText().toString().trim();
        String anoTexto = edtAno.getText().toString().trim();

        if (titulo.isEmpty()) {
            edtTitulo.setError("Informe o título");
            edtTitulo.requestFocus();
            return;
        }

        if (autor.isEmpty()) {
            edtAutor.setError("Informe o autor");
            edtAutor.requestFocus();
            return;
        }

        if (spGenero.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Selecione um gênero",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (anoTexto.isEmpty()) {
            edtAno.setError("Informe o ano de publicação");
            edtAno.requestFocus();
            return;
        }

        if (spStatus.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Selecione o status de leitura",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        int ano;

        try {
            ano = Integer.parseInt(anoTexto);
        } catch (NumberFormatException e) {
            edtAno.setError("Ano inválido");
            edtAno.requestFocus();
            return;
        }

        if (ano <= 0) {
            edtAno.setError("Informe um ano válido");
            edtAno.requestFocus();
            return;
        }

        String genero = spGenero.getSelectedItem().toString();
        String leitura = spStatus.getSelectedItem().toString();

        boolean favorito = chkFavorito.isChecked();

        Livro livro = new Livro(
                titulo,
                autor,
                genero,
                ano,
                leitura,
                favorito
        );

        if (idLivroEdicao == null) {

            // Cadastrar novo livro
            livrosRef.add(livro)
                    .addOnSuccessListener(documentReference -> {

                        Toast.makeText(this,
                                "Livro cadastrado com sucesso!",
                                Toast.LENGTH_SHORT).show();

                        limparCampos();

                    })
                    .addOnFailureListener(e -> {

                        Toast.makeText(this,
                                "Erro ao cadastrar: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();

                    });

        } else {

            // Atualizar livro existente
            livrosRef.document(idLivroEdicao)
                    .set(livro)
                    .addOnSuccessListener(aVoid -> {

                        Toast.makeText(this,
                                "Livro atualizado com sucesso!",
                                Toast.LENGTH_SHORT).show();

                        limparCampos();

                    })
                    .addOnFailureListener(e -> {

                        Toast.makeText(this,
                                "Erro ao atualizar: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();

                    });
        }
    }

    // Buscar livros no Firebase
    private void listarLivros() {

        livrosRef.addSnapshotListener(this, (dados, erro) -> {

            if (erro != null) {
                Toast.makeText(this,
                        "Erro ao carregar livros: " + erro.getMessage(),
                        Toast.LENGTH_LONG).show();
                return;
            }

            if (dados != null) {

                todosLivros.clear();
                todosIds.clear();

                for (DocumentSnapshot documento : dados.getDocuments()) {

                    Livro livro = documento.toObject(Livro.class);

                    if (livro != null) {
                        todosLivros.add(livro);
                        todosIds.add(documento.getId());
                    }
                }

                atualizarLista();
            }
        });
    }

    // Atualizar a lista com os filtros
    private void atualizarLista() {

        listaNomes.clear();
        idsExibidos.clear();
        livrosExibidos.clear();

        String busca = edtBusca.getText().toString()
                .toLowerCase(Locale.ROOT).trim();

        String generoSelecionado =
                spFiltroGenero.getSelectedItem().toString();

        for (int i = 0; i < todosLivros.size(); i++) {

            Livro livro = todosLivros.get(i);

            boolean correspondeBusca = livro.getTitulo() != null &&
                    livro.getTitulo().toLowerCase(Locale.ROOT)
                            .contains(busca);

            boolean correspondeGenero =
                    generoSelecionado.equals("Todos os gêneros") ||
                            generoSelecionado.equals(livro.getGenero());

            if (correspondeBusca && correspondeGenero) {

                String favorito = "";

                if (livro.isFavorito()) {
                    favorito = "\nFavorito";
                }

                String informacoes =
                        "Título: " + livro.getTitulo() +
                                "\nAutor: " + livro.getAutor() +
                                "\nGênero: " + livro.getGenero() +
                                "\nAno: " + livro.getAno() +
                                "\nStatus: " + livro.getStatus() +
                                favorito;

                listaNomes.add(informacoes);
                idsExibidos.add(todosIds.get(i));
                livrosExibidos.add(livro);
            }
        }

        adapterLista.notifyDataSetChanged();

        txtQuantidade.setText(
                "Total de livros: " + todosLivros.size()
        );
    }

    // Carregar dados para edição
    private void editarLivro(int position) {

        Livro livro = livrosExibidos.get(position);

        idLivroEdicao = idsExibidos.get(position);

        edtTitulo.setText(livro.getTitulo());
        edtAutor.setText(livro.getAutor());
        edtAno.setText(String.valueOf(livro.getAno()));

        spGenero.setSelection(
                generos.indexOf(livro.getGenero())
        );

        spStatus.setSelection(
                status.indexOf(livro.getStatus())
        );

        chkFavorito.setChecked(livro.isFavorito());

        btnSalvar.setText("Atualizar Livro");

        Toast.makeText(this,
                "Altere os dados e clique em Atualizar Livro",
                Toast.LENGTH_LONG).show();
    }

    // Confirmar exclusão
    private void confirmarExclusao(int position) {

        Livro livro = livrosExibidos.get(position);

        String id = idsExibidos.get(position);

        new AlertDialog.Builder(this)
                .setTitle("Excluir livro")
                .setMessage("Deseja realmente excluir o livro "
                        + livro.getTitulo() + "?")
                .setPositiveButton("Sim", (dialog, which) -> {
                    excluirLivro(id);
                })
                .setNegativeButton("Não", null)
                .show();
    }

    // Excluir livro do Firebase
    private void excluirLivro(String id) {

        livrosRef.document(id)
                .delete()
                .addOnSuccessListener(aVoid -> {

                    Toast.makeText(this,
                            "Livro excluído com sucesso!",
                            Toast.LENGTH_SHORT).show();

                    if (id.equals(idLivroEdicao)) {
                        limparCampos();
                    }

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(this,
                            "Erro ao excluir: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();

                });
    }

    // Limpar formulário
    private void limparCampos() {

        edtTitulo.setText("");
        edtAutor.setText("");
        edtAno.setText("");

        spGenero.setSelection(0);
        spStatus.setSelection(0);

        chkFavorito.setChecked(false);

        idLivroEdicao = null;

        btnSalvar.setText("Salvar Livro");
    }
}
