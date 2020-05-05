
run:
	npm run serve

unit:
	npm run test:unit

integ:
	npm run test:integ

only:
	npm run test:only

clear:
	npx jest --clearCache


test-server:
	node tests/WebSocketServer.js


test-config:
	npx jest --debug

#________________________________________________________________________________
# Test specific modules
#________________________________________________________________________________
task:
	./node_modules/.bin/vue-cli-service test:unit --testPathPattern="TaskStore.spec.js"
	#npx vue-cli-service test:unit --testPathPattern="TaskStore.spec.js"
	#npx vue-cli-service test:unit --testPathPattern="TaskStore.spec.js"