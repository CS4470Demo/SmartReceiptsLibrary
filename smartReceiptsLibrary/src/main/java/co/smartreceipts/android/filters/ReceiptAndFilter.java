package co.smartreceipts.android.filters;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import co.smartreceipts.android.model.Receipt;

/**
 * A filter implementation of {@link AndFilter} for {@link co.smartreceipts.android.model.Receipt}
 * 
 * @author Will Baumann
 * @since July 08, 2014
 * 
 */
public class ReceiptAndFilter extends AndFilter<Receipt>{

	public ReceiptAndFilter() {
		super();
	}
	
	public ReceiptAndFilter(List<Filter<Receipt>> filters) {
		super(filters);
	}
	
	protected ReceiptAndFilter(JSONObject json) throws JSONException {
		super(json);
	}
	
	
	@Override
	Filter<Receipt> getFilter(JSONObject json) throws JSONException {
		return FilterFactory.getReceiptFilter(json);
	}

}
