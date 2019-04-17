let app = new Vue({
    el: '#app',
    data: {
        query: '',
        results: [],
        suggestions: [],
        stats: {
            queryTimeMs: 0,
            queryResults: 0
        },
        loading: {
            results: false,
            suggestions: false
        }
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
                url: 'http://localhost:8888/suggest',
                data: { query: self.query }
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

        fetchNewResults() {
            let self = this;

            if (self.loading.results || self.query.length < 1) {
                return;
            }            
            const t = new Date().getTime();
            self.loading.results = true;
            self.results = [];

            $.ajax({
                url: 'http://localhost:8888/businesses',
                data: { query: self.query }
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
        }
    },

    watch: {
        query() {
            this.fetchSuggestions()
        }
    }
});
