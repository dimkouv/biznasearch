let app = new Vue({
    el: '#app',
    data: {
        isLoading: false,
        query: '',
        results: [],
        stats: {
            queryTimeMs: 0,
            queryResults: 0,
            lastQueryTime: 0
        },
        searchJob: null
    },

    methods: {
        fetchNewResults() {
            let self = this;
            const t = new Date().getTime();
            // allow one query per 500ms
            console.log(t - this.stats.lastQueryTime);

            if (t - this.stats.lastQueryTime < 500) {
                console.log('isin');

                if (this.searchJob !== null) {
                    clearTimeout(this.searchJob);
                }
                console.log('New job: ', this.query);
                this.searchJob = setTimeout(() => {
                    self.fetchNewResults()
                }, 500 - (t - this.stats.lastQueryTime));
                return
            }

            this.stats.lastQueryTime = t;
            this.isLoading = true;
            this.results = [];

            $.ajax({
                url: 'http://localhost:8888/businesses',
                data: {
                    query: this.query
                }
            })
                .then(res => {
                    this.stats.queryTimeMs = new Date().getTime() - t;
                    this.stats.queryResults = res.length;

                    this.results = res;
                })
                .catch(err => {
                    console.log(err)
                })
                .then(() => {
                    this.isLoading = false
                })
        }
    },

    watch: {
        query() {
            if (!this.isLoading) {
                this.fetchNewResults();
            }
        }
    }
});
