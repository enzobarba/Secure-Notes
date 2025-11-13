package com.example.securenotes.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter; // Adapter moderno per aggiornamenti automatici
import androidx.recyclerview.widget.RecyclerView;
import java.text.DateFormat;
import java.util.Date;
import android.graphics.Color;
import androidx.core.graphics.ColorUtils;

import com.example.securenotes.model.Note;
import com.example.securenotes.databinding.ItemNoteBinding;

// ListAdapter gestisce la lista e le animazioni automaticamente
public class NoteAdapter extends ListAdapter<Note, NoteAdapter.NoteViewHolder> {

    // Interfaccia "callback" per notificare il Fragment di un click
    public interface OnItemClickListener {
        void onItemClick(Note note);
    }
    private OnItemClickListener listener;

    /*
    interfaccia callback che
    passa sia la Nota che la Vista (la CardView),
    che ci serve per ancorare il PopupMenu.
    */
    public interface OnItemLongClickListener {
        void onItemLongClick(Note note, View view);
    }
    private OnItemLongClickListener longClickListener;

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public NoteAdapter() {
        super(DIFF_CALLBACK); // Passa il gestore delle differenze
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // "Gonfia" (crea) il layout XML 'item_note.xml' usando ViewBinding
        ItemNoteBinding binding = ItemNoteBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new NoteViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note currentNote = getItem(position);
        holder.bind(currentNote);
    }

    // ViewHolder: tiene in memoria le View di una singola riga
    class NoteViewHolder extends RecyclerView.ViewHolder {

        private final ItemNoteBinding binding;

        public NoteViewHolder(@NonNull ItemNoteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            // Imposta il click listener una sola volta qui, per efficienza
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(getItem(position));
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (longClickListener != null && position != RecyclerView.NO_POSITION) {
                    // Passa la nota e la vista (v)
                    longClickListener.onItemLongClick(getItem(position), v);
                    return true; //
                }
                return false;
            });
        }

        public void bind(Note note) {
            binding.textViewTitle.setText(note.title);
            binding.textViewContent.setText(note.content);
            binding.textViewTimestamp.setText(formatTimestamp(note.timestamp));
            binding.getRoot().setCardBackgroundColor(note.color);
            if (note.isPinned) {
                binding.imageViewPin.setVisibility(View.VISIBLE);
            } else {
                binding.imageViewPin.setVisibility(View.GONE);
            }
        }
    }

    // DIFF_CALLBACK: Dice a ListAdapter come calcolare le differenze
    // in modo efficiente, senza ridisegnare l'intera lista.
    private static final DiffUtil.ItemCallback<Note> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Note>() {

                @Override
                public boolean areItemsTheSame(@NonNull Note oldItem, @NonNull Note newItem) {
                    // Controlla se è lo stesso oggetto (tramite ID univoco)
                    return oldItem.id == newItem.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull Note oldItem, @NonNull Note newItem) {
                    // Controlla se i dati visibili sono cambiati
                    return oldItem.title.equals(newItem.title) &&
                            oldItem.content.equals(newItem.content) &&
                            oldItem.timestamp == newItem.timestamp &&
                            oldItem.color == newItem.color &&
                            oldItem.isPinned == newItem.isPinned;
                }
            };

    private String formatTimestamp(long millis) {
        // 'DateFormat.getDateTimeInstance()' usa il formato
        // data e ora predefinito per la lingua e la regione
        // del telefono dell'utente (es. "11/11/25, 15:30" o "11-11-2025 3:30 PM")
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        return df.format(new Date(millis));
    }
}