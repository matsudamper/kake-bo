CREATE TABLE user_received_mails
(
    user_received_mail_id int auto_increment                   not null,
    user_id               int                                  not null,
    created_datetime      datetime default current_timestamp() not null,
    raw_text              text                                 not null
)
