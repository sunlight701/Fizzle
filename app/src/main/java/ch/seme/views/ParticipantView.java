package ch.seme.views;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import ch.seme.databinding.ItemConferenceParticipantBinding;
import io.reactivex.disposables.Disposable;

public class ParticipantView extends RecyclerView.ViewHolder {
    public final ItemConferenceParticipantBinding binding;
    public Disposable disposable = null;

    public ParticipantView(@NonNull ItemConferenceParticipantBinding b) {
        super(b.getRoot());
        binding = b;
    }
}
