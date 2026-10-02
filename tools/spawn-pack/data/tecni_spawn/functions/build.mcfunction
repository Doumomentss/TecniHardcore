scoreboard objectives add th_spawn dummy
execute unless score #built th_spawn matches 1.. run function tecni_spawn:prepare
