DROP TABLE IF EXISTS consultation_view;

CREATE VIEW consultation_view AS
SELECT
    c.id,
    COALESCE(COUNT(ca.id), 0)::bigint AS attendance_count,
    c.end_time,
    c.meeting_link,
    c.one_time_date,
    c.online,
    c.start_time,
    c.status,
    c.student_instructions,
    c.type,
    c.weekly_day_of_week,
    c.professor_id,
    c.room_name
FROM consultation c
LEFT JOIN consultation_attendance ca ON ca.consultation_id = c.id
GROUP BY
    c.id,
    c.end_time,
    c.meeting_link,
    c.one_time_date,
    c.online,
    c.start_time,
    c.status,
    c.student_instructions,
    c.type,
    c.weekly_day_of_week,
    c.professor_id,
    c.room_name;
