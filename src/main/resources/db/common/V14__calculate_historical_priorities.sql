UPDATE booking
SET priority = LEAST(10, 1
    + CASE WHEN end_time - start_time >= INTERVAL '3 hours' THEN 2 ELSE 0 END
    + CASE WHEN attendees > 5 THEN 3 ELSE 0 END
    + CASE WHEN requires_equipment THEN 2 ELSE 0 END
    + CASE WHEN is_recurring THEN 2 ELSE 0 END);
