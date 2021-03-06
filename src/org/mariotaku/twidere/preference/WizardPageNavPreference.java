package org.mariotaku.twidere.preference;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import org.mariotaku.twidere.R;

public final class WizardPageNavPreference extends Preference {

	public WizardPageNavPreference(Context context) {
		this(context, null);
	}

	public WizardPageNavPreference(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.preferenceStyle);
	}

	public WizardPageNavPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setLayoutResource(R.layout.wizard_page_nav_item);
	}

	@Override
	protected void onBindView(View view) {
		super.onBindView(view);
		final TextView text1 = (TextView) view.findViewById(android.R.id.text1);
		text1.setText(getTitle());
	}

}