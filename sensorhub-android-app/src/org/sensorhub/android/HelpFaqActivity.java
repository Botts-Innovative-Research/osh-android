package org.sensorhub.android;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class HelpFaqActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_faq);

        MaterialToolbar toolbar = findViewById(R.id.help_faq_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        RecyclerView recyclerView = findViewById(R.id.faq_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new FaqAdapter(buildFaqItems()));
    }

    private List<FaqItem> buildFaqItems() {
        List<FaqItem> items = new ArrayList<>();
        String[] questions = getResources().getStringArray(R.array.faq_questions);
        String[] answers = getResources().getStringArray(R.array.faq_answers);
        String[] categories = getResources().getStringArray(R.array.faq_categories);

        String currentCategory = "";
        for (int i = 0; i < questions.length; i++) {
            String category = categories[i];
            if (!category.equals(currentCategory)) {
                items.add(new FaqItem(category, null, true));
                currentCategory = category;
            }
            items.add(new FaqItem(questions[i], answers[i], false));
        }
        return items;
    }

    private static class FaqItem {
        final String text;
        final String answer;
        final boolean isHeader;
        boolean expanded;

        FaqItem(String text, String answer, boolean isHeader) {
            this.text = text;
            this.answer = answer;
            this.isHeader = isHeader;
            this.expanded = false;
        }
    }

    private static class FaqAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_HEADER = 0;
        private static final int TYPE_ITEM = 1;

        private final List<FaqItem> items;

        FaqAdapter(List<FaqItem> items) {
            this.items = items;
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).isHeader ? TYPE_HEADER : TYPE_ITEM;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == TYPE_HEADER) {
                View view = inflater.inflate(R.layout.item_faq_header, parent, false);
                return new HeaderViewHolder(view);
            } else {
                View view = inflater.inflate(R.layout.item_faq_entry, parent, false);
                return new ItemViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            FaqItem item = items.get(position);
            if (holder instanceof HeaderViewHolder) {
                ((HeaderViewHolder) holder).title.setText(item.text);
            } else if (holder instanceof ItemViewHolder) {
                ItemViewHolder itemHolder = (ItemViewHolder) holder;
                itemHolder.question.setText(item.text);
                itemHolder.answer.setText(item.answer);
                itemHolder.answer.setVisibility(item.expanded ? View.VISIBLE : View.GONE);
                itemHolder.expandIcon.setImageResource(
                        item.expanded ? R.drawable.ic_expand_less : R.drawable.ic_expand_more);
                itemHolder.itemView.setOnClickListener(v -> {
                    item.expanded = !item.expanded;
                    notifyItemChanged(position);
                });
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class HeaderViewHolder extends RecyclerView.ViewHolder {
            final TextView title;

            HeaderViewHolder(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.faq_header_title);
            }
        }

        static class ItemViewHolder extends RecyclerView.ViewHolder {
            final TextView question;
            final TextView answer;
            final ImageView expandIcon;

            ItemViewHolder(View itemView) {
                super(itemView);
                question = itemView.findViewById(R.id.faq_question);
                answer = itemView.findViewById(R.id.faq_answer);
                expandIcon = itemView.findViewById(R.id.faq_expand_icon);
            }
        }
    }
}
