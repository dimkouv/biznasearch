#!/bin/bash

# example usage of `jq` for exporting to tsv
jq -r '[.business_id, .name, .latitude, .longitude, .city, .stars, .review_count, .address, .postal_code, .categories] | @tsv' \
    business.json                               \
    | tr "\"" "'"                               \
    > businesses.tsv

jq -r '[.review_id, .business_id, .stars, .date, .text, .useful, .funny, .cool] | @tsv' \
    review.json                               \
    | tr "\"" "'"                               \
    > reviews.tsv

jq -r '[.text, .date, .compliment_count, .business_id] | @tsv' \
    tip.json                               \
    | tr "\"" "'"                               \
    > tips.tsv

