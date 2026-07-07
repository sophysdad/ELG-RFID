package dngsoftware.elgrfid;

import static dngsoftware.elgrfid.Utils.TEMP_PICKER_COUNT;
import static dngsoftware.elgrfid.Utils.pickerIndexToTemp;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

public class tempPickerAdapter extends RecyclerView.Adapter<tempPickerAdapter.ViewHolder> {

    private final Context context;
    private int selectedPosition;

    public tempPickerAdapter(Context context, int initialPosition) {
        this.context = context;
        this.selectedPosition = initialPosition;
    }

    public void setSelectedPosition(int position) {
        selectedPosition = position;
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.temp_picker_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int temp = pickerIndexToTemp(position);
        holder.itemText.setText(temp + "\u00B0C");
        boolean selected = position == selectedPosition;
        holder.itemText.setTextColor(ContextCompat.getColor(
                context, selected ? R.color.text_main : R.color.text_alt));
        holder.itemText.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
        holder.itemText.setAlpha(selected ? 1f : 0.55f);
    }

    @Override
    public int getItemCount() {
        return TEMP_PICKER_COUNT;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView itemText;

        ViewHolder(View itemView) {
            super(itemView);
            itemText = itemView.findViewById(R.id.temp_picker_item_text);
        }
    }
}