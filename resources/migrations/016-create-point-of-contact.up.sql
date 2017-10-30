CREATE TABLE point_of_contact (
       id SERIAL PRIMARY KEY,
       name VARCHAR(100),
       phone VARCHAR(20),
       email VARCHAR(100),
       client_id int references client(id)
);
