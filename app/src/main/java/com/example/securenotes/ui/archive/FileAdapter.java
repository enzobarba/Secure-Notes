package com.example.securenotes.ui.archive;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securenotes.databinding.ItemFileBinding;
import java.io.File;

public class FileAdapter extends ListAdapter<File, FileAdapter.FileViewHolder> {

    // Interfaccia callback per il click (per aprire il file)
    public interface OnFileClickListener {
        void onFileClick(File file);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(File file, View view);
    }
    private OnItemLongClickListener longClickListener;

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    private OnFileClickListener listener;

    public void setOnItemClickListener(OnFileClickListener listener) {
        this.listener = listener;
    }

    public FileAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFileBinding binding = ItemFileBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new FileViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    // tiene in memoria le View di 'item_file.xml'
    class FileViewHolder extends RecyclerView.ViewHolder {

        private final ItemFileBinding binding;

        public FileViewHolder(@NonNull ItemFileBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onFileClick(getItem(position));
                }
            });
            //listener per pressione lunga
            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (longClickListener != null && position != RecyclerView.NO_POSITION) {
                    // Chiama il nuovo listener
                    // Passa il file e la vista (v)
                    longClickListener.onItemLongClick(getItem(position), v);
                    return true;
                }
                return false; // evento non gestito
            });
        }

        // Imposta il nome del file nella TextView
        public void bind(File file) {
            binding.textViewFileName.setText(file.getName());
        }
    }

    // Confronta i file in base al loro percorso
    private static final DiffUtil.ItemCallback<File> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<File>() {

                @Override
                public boolean areItemsTheSame(@NonNull File oldItem, @NonNull File newItem) {
                    return oldItem.getPath().equals(newItem.getPath());
                }

                @Override
                public boolean areContentsTheSame(@NonNull File oldItem, @NonNull File newItem) {
                    return oldItem.getName().equals(newItem.getName()) &&
                            oldItem.lastModified() == newItem.lastModified();
                }
            };
}