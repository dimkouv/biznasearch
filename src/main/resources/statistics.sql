/* Count businesses, reviews and tips per city */
select t1."city", "business_count", "reviews_count", "tips_count" from (
	select b."city" as "city", count(b.id) as "business_count" from businesses b
	group by b.city
) t1 inner join (
	select b.city as "city", count(r.id) as "reviews_count" from businesses b
	inner join reviews r on (r.business_id=b.id)
	group by b.city
) t2 on (t1."city" = t2."city")
    inner join (
    select b.city as "city", count(t.id) as "tips_count" from businesses b
	inner join tips t on (t.business_id=b.id)
	group by b.city
) t3 on (t3."city" = t2."city")
order by "reviews_count" desc limit 7
