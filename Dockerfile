FROM amazoncorretto:22-alpine
CMD mkdir src
COPY frontend/jsApp/build/distributions src

COPY backend/build/distributions/backend.tar backend.tar
RUN tar -xf backend.tar
RUN rm backend.tar
RUN mkdir logs

ENTRYPOINT ["./backend/bin/backend"]
