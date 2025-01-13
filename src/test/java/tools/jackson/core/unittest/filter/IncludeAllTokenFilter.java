package tools.jackson.core.unittest.filter;

import tools.jackson.core.filter.TokenFilter;

// Needed due to protected constructor
class IncludeAllTokenFilter extends TokenFilter {
    public IncludeAllTokenFilter() {
        super();
    }

    @Override
    protected boolean _includeScalar() {
        return super._includeScalar();
    }
}
