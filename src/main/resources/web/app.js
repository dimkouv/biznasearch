let app = new Vue({
    el: '#app',
    data: {
        apiHost: 'http://localhost:8888',
        query: '',
        resultsNum: 10,
        orderBy: '-clicks',
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
        selectedBusiness: null
    },

    methods: {
        fetchSuggestions() {
            let self = this;

            if (self.loading.suggestions || self.query.length < 1) {
                return;
            }
            const t = new Date().getTime();
            self.loading.suggestions = true;
            self.results = [];

            $.ajax({
                url: this.apiHost + '/spell-check',
                data: {query: self.query}
            })
                .then(res => {
                    self.suggestions = res;
                })
                .catch(err => {
                    console.log(err);
                })
                .then(() => {
                    self.loading.suggestions = false;
                })
        },

        fetchQuerySuggestions() {
            let self = this;

            $.ajax({
                url: this.apiHost + '/query-suggest',
                data: {query: self.query}
            })
                .then(res => {
                    self.querySuggestions = res;
                })
        },

        fetchFromQuerySuggestion(text) {
            this.query = text;
            this.fetchNewResults();
            setTimeout(() => {
                this.querySuggestions = []
            }, 200)
        },

        fetchResults() {
            let self = this;

            setTimeout(() => {
                this.querySuggestions = []
            }, 200);

            if (self.loading.results || self.query.length < 1) {
                return;
            }
            const t = new Date().getTime();
            self.loading.results = true;
            self.results = [];
            self.page = 1;

            $.ajax({
                url: this.apiHost + '/search',
                data: {
                    query: self.query,
                    orderBy: self.orderBy,
                    'results-num': self.resultsNum
                }
            })
                .then(res => {
                    self.stats.queryTimeMs = new Date().getTime() - t;
                    self.stats.queryResults = res.length;
                    self.results = res;
                })
                .catch(err => {
                    console.log(err);
                })
                .then(() => {
                    self.loading.results = false;
                })
        },

        populateResults() {
            this.resultsNum += 10;
            this.fetchResults();
        },

        newResults() {
            this.resultsNum = 10;
            this.fetchResults();
        }
    },

    watch: {
        query() {
            this.fetchSuggestions();
            this.fetchQuerySuggestions()
        },

        selectedBusiness() {
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
            this.newResults()
        }
    }
});
