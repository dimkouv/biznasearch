let app = new Vue({
    el: '#app',
    data: {
        apiHost: 'http://127.0.0.1:8888',
        query: '',
        resultsNum: 10,
        orderBy: '',
        results: [],
        suggestions: [],
        querySuggestions: [],
        stats: {
            queryTimeMs: 0,
            queryResults: 0
        },
        loading: {
            results: false,
            suggestions: false
        },
        selectedBusiness: null,
        statsForGeeks: null
    },

    mounted: function () {
        this.runStatsPolling();
    },

    methods: {
        fetchStatsForGeeks() {
            /** Sets query suggestions, ideally when user is typing */
            $.ajax({
                    url: this.apiHost + '/server-stats'
                })
                .then(res => {
                    this.statsForGeeks = res;
                })
        },

        fetchSpellSuggestions(query) {
            /** Returns spell check suggestions for a target query. */
            if (this.loading.suggestions || this.query.length < 1) {
                return;
            }
            const t = new Date().getTime();
            this.loading.suggestions = true;
            this.results = [];

            $.ajax({
                    url: this.apiHost + '/spell-check',
                    data: {
                        query: query
                    }
                })
                .then(res => {
                    this.suggestions = res;
                })
                .catch(err => {
                    alert('Error while fetching spell suggestions.')
                })
                .then(() => {
                    this.loading.suggestions = false;
                })
        },

        fetchQuerySuggestions(query) {
            /** Sets query suggestions, ideally when user is typing */
            $.ajax({
                    url: this.apiHost + '/query-suggest',
                    data: {
                        query: query
                    }
                })
                .then(res => {
                    this.querySuggestions = res;
                })
        },

        fetchFromQuerySuggestion(text) {
            /** Fetch results by clicking on query suggestion */
            this.query = text;
            this.newResults();
            setTimeout(() => {
                this.querySuggestions = []
            }, 200)
        },

        fetchResults() {
            /** fetch results */
            setTimeout(() => {
                this.querySuggestions = []
            }, 200);

            if (this.loading.results || this.query.length < 1) {
                return;
            }
            const t = new Date().getTime();
            this.loading.results = true;
            this.results = [];
            this.page = 1;

            $.ajax({
                    url: this.apiHost + '/search',
                    data: {
                        query: this.query,
                        'order-by': this.orderBy,
                        'results-num': this.resultsNum
                    }
                })
                .then(res => {
                    this.stats.queryTimeMs = new Date().getTime() - t;
                    this.stats.queryResults = res.length;
                    this.results = res;
                })
                .catch(err => {
                    alert('Error while fetching business results.')
                })
                .then(() => {
                    this.loading.results = false;
                })
        },

        populateResults() {
            /** fetch new reuslts by clicking 'load more' button */
            this.resultsNum += 10;
            this.fetchResults();
        },

        newResults() {
            /** fetch results normally */
            this.resultsNum = 10;
            this.fetchResults();
        },

        newResultsFromCategory(category) {
            /** fetch results by clicking on a category */
            this.query = `categories: "${category}"`;
            this.newResults();
        },

        runStatsPolling() {
            setInterval(() => {
                if (this.statsForGeeks) {
                    this.fetchStatsForGeeks();
                }
            }, 1000);
        }
    },

    watch: {
        query() {
            /** fetch suggestions as the user is typing */
            this.fetchSpellSuggestions(this.query);
            this.fetchQuerySuggestions(this.query);
        },

        selectedBusiness() {
            /** add business click stats when a business is opened */
            if (this.selectedBusiness !== null) {
                $.ajax({
                    method: 'post',
                    url: this.apiHost + '/click',
                    data: {
                        'business-id': this.selectedBusiness.id
                    }
                });
                this.selectedBusiness.clicks++
            }
        },

        orderBy() {
            /** fetch ordered results */
            this.newResults()
        }
    }
});